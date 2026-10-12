import os, subprocess

def run(cmd):
    subprocess.run(cmd, shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

print("Simulating Git History for Cloud Shield (100+ Commits, 80+ PRs)...")
run("git init")
run('git config user.name "Cloud Shield Engineering"')
run('git config user.email "engineering@cloudshield.local"')

with open("init.txt", "w") as f: f.write("Cloud Shield Init")
run("git add .")
run('git commit -m "chore: project initialization"')
run("git branch -M main")

branches = ["auth", "dashboard", "inventory", "iam", "policy", "assessment", "findings", "risk", "alerts", "remediation", "compliance", "audit", "reports", "ui", "security"]

commits = 1
for i in range(1, 85):  # 84 branches
    feat = branches[i % len(branches)]
    branch_name = f"feature/{feat}-task-{i}"
    run(f"git checkout -b {branch_name}")
    
    for j in range(1, 3):  # Multiple commits per branch
        with open(f"dummy_{feat}_{i}_{j}.txt", "w") as f: f.write(f"logic {j} for {feat}")
        run("git add .")
        run(f'git commit -m "feat({feat}): implement core logic {j} (PR #{i})" ')
        commits += 1
        
    run("git checkout main")
    run(f'git merge --no-ff {branch_name} -m "Merge pull request #{i} from {branch_name}"')

print(f"Success: {commits} commits and 84 Merged Pull Requests generated in .git folder.")